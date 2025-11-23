job "auth-service" {
  datacenters = ["dc1"]
  type = "service"

  group "auth-service-group" {
    count = 3  # 인스턴스 개수

    network {
      mode = "bridge"

      port "grpc" {
        to = 50053
      }

      port "http" {
        to = 9053
      }
    }

    service {
      name = "auth-service"
      port = "grpc"

      tags = [
        "grpc",
        "auth",
      ]

      connect {
        sidecar_service {
          proxy {
            upstreams {
              destination_name = "postgres"
              local_bind_port  = 5432
            }
            upstreams {
              destination_name = "redis"
              local_bind_port  = 6379
            }
            upstreams {
              destination_name = "kafka"
              local_bind_port  = 9092
            }
          }
        }
      }

      check {
        name     = "health-check"
        type     = "http"
        port     = "http"
        path     = "/q/health/live"
        interval = "10s"
        timeout  = "2s"
      }
    }

    task "auth-service" {
      driver = "docker"

      config {
        image = "suehunpark/auth-service:latest"

        # 핵심: 재시작할 때마다 최신 이미지 강제 풀링
        force_pull = true

        ports = ["grpc", "http"]
      }

      env {
        # Redis 설정
        REDIS_HOSTS = "redis://${NOMAD_UPSTREAM_ADDR_redis}"

        # Database 설정
        DB_URL = "jdbc:postgresql://${NOMAD_UPSTREAM_ADDR_postgres}/hts_auth"
        DB_USER = "hts"
        # DB_PASSWORD는 Vault에서 주입하거나 아래에 직접 설정

        # Kafka 설정
        KAFKA_BOOTSTRAP_SERVERS = "${NOMAD_UPSTREAM_ADDR_kafka}"
      }

      # Vault에서 DB 비밀번호 주입 (선택사항)
      # template {
      #   data = <<EOH
      # DB_PASSWORD="{{ with secret "secret/data/auth-service" }}{{ .Data.data.db_password }}{{ end }}"
      # EOH
      #   destination = "secrets/db.env"
      #   env         = true
      # }

      resources {
        cpu    = 500  # MHz
        memory = 512  # MB
      }

      # 롤링 업데이트 설정
      update {
        max_parallel     = 1
        health_check     = "checks"
        min_healthy_time = "10s"
        healthy_deadline = "5m"
        auto_revert      = true
        canary           = 1
      }
    }
  }
}
