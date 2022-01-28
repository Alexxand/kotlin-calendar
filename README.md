How to run:
./gradlew docker
docker-compose -f docker-compose-start-server.yml --project-name calendar up

If you need to undo last applied migration use:
docker-compose -f docker-compose-undo-last-migration.yml --project-name undo-migration up