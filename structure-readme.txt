1. Client sends a JSON request.
2. Controller receives the JSON and maps it to a DTO (TicketRequest).
3. Controller passes that DTO to the Service.
4. Service applies business logic, converts the DTO into a Model (Ticket), and tells the Repository to save it.
5. Repository saves the Model to the database.
6. Service gets the saved Model back, converts it into a safe DTO (TicketResponse), and hands it to the Controller.
7. Controller sends the DTO back to the client as a JSON response-