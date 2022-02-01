Для сборки и запуска нужно выполнить:  
./gradlew docker  
docker-compose -f docker-compose-start-server.yml --project-name calendar up  

Если с первого раза не получилось (сервер не отвечает), то можно нажать Ctrl+C и попробовать снова.  
Я не успел разобраться с этой проблемой.

Сервер запускается на localhost:8000

Доступны следующие запросы:  
POST localhost:8000/users  
{  
"name": "Alex",  
"lastname": "Petukhov"  
}

GET localhost:8000/users

DELETE localhost:8000/users/{userId}

POST localhost:8000/meetings  
{  
"meetingOrganizerId": "dbcc9e2a-9d6f-4d7b-9c35-a787f0cd137b",  
"invitations": [{"id": "2b2d314f-b0b4-40b6-a373-68dc13d036e6"}],  
"startTime": "2022-09-30T14:14:22Z",  
"endTime": "2022-09-30T14:18:22Z"  
}

GET localhost:8000/meetings

GET localhost:8000/meetings/{meetingId}

POST localhost:8000/users/{userId}/meetings/{meetingId}/makeActionOnInvitation?invitationAction=ACCEPT  
POST localhost:8000/users/{userId}/meetings/{meetingId}/makeActionOnInvitation?invitationAction=REJECT  

GET localhost:8000/users/{userId}/meetings?startTime=2022-01-29T14:50:22.078722Z&endTime=2022-01-30T23:50:22.078722Z&includeNotAccepted=true  
(все три query параметра опциональны, если начало или конец интервала не переданы,  
то они равны -inf и +inf соответственно;  
встречи, на которые пользователь пока не принял приглашение, по умолчанию не возвращаются)  

GET localhost:8000/meetings/nearestInterval  
{  
"minDuration": "PT10M", //формат java.time.Duration.parse(CharSequence text)  
"userIds": [{"id": "04bd9ea0-e420-44fa-8cde-fd7466b84355"}]  
}  

DELETE localhost:8000/meetings/{meetingId}
