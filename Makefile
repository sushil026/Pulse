.PHONY: up down clean logs ps db restart

up:
	cd infrastructure/docker && docker compose up -d

down:
	cd infrastructure/docker && docker compose down

clean:
	cd infrastructure/docker && docker compose down -v

logs:
	cd infrastructure/docker && docker compose logs -f

ps:
	cd infrastructure/docker && docker compose ps

db:
	docker exec -it pulse-postgres psql -U pulse -d pulse_db

restart: down up
