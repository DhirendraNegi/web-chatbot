'use strict';

var usernamePage = document.querySelector('#username-page');
var chatPage = document.querySelector('#chat-page');
var usernameForm = document.querySelector('#usernameForm');
var messageForm = document.querySelector('#messageForm');
var messageInput = document.querySelector('#message');
var messageArea = document.querySelector('#messageArea');
var connectingElement = document.querySelector('.connecting');

var stompClient = null;
var username = null;
var i = 0;

var colors = [
    '#2196F3', '#32c787', '#00BCD4', '#ff5652',
    '#ffc107', '#ff85af', '#FF9800', '#39bbb0'
];

function connect(event) {
    username = document.querySelector('#name').value.trim();

    if(username) {
        usernamePage.classList.add('hidden');
        chatPage.classList.remove('hidden');

        var socket = new SockJS('/websocket');
        stompClient = Stomp.over(socket);

        stompClient.connect({}, onConnected, onError);
    }
    event.preventDefault();
}


function onConnected() {
    // Subscribe to the Public Topic
    stompClient.subscribe('/topic/public', onMessageReceived);

    // Tell your username to the server
    stompClient.send("/app/chat.register",
        {},
        JSON.stringify({sender: username, type: 'JOIN'})
    )

    connectingElement.classList.add('hidden');
}


function onError(error) {
    connectingElement.textContent = 'Contact Admin.';
    connectingElement.style.color = 'red';
}


function send(event) {
    var messageContent = messageInput.value.trim();
     if(messageContent && stompClient) {
        var chatMessage = {
            sender: username,
            content: messageInput.value,
            type: 'CHAT'
        };

        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));

        if ( i == 0 ){
        i = 1;
        stompClient.send("/app/chat.register",
                {},
                JSON.stringify({sender: 'Virtual Assist (Hiyya)', type: 'JOIN'})
            )
        chatMessage = {
                    sender: 'Hiyya',
                    content: 'Welcome to Virtual Assistant. How can I help you today?',
                    type: 'CHAT'
                };
        stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        } else {

            var resposeContent = makeSynchronousRequest('http://localhost:8080/chat/response', 'POST', chatMessage.content);

            chatMessage = {
                            sender: 'Hiyya',
                            content: resposeContent,
                            type: 'CHAT'
                        };
            stompClient.send("/app/chat.send", {}, JSON.stringify(chatMessage));
        }

        messageInput.value = '';
    }
    event.preventDefault();
}


function onMessageReceived(payload) {
    var message = JSON.parse(payload.body);

    var messageElement = document.createElement('li');

    if(message.type === 'JOIN') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' joined!';
    } else if (message.type === 'LEAVE') {
        messageElement.classList.add('event-message');
        message.content = message.sender + ' left!';
    } else {
        messageElement.classList.add('chat-message');

        var avatarElement = document.createElement('i');
        var avatarText = document.createTextNode(message.sender[0]);
        avatarElement.appendChild(avatarText);
        avatarElement.style['background-color'] = getAvatarColor(message.sender);

        messageElement.appendChild(avatarElement);

        var usernameElement = document.createElement('span');
        var usernameText = document.createTextNode(message.sender);
        usernameElement.appendChild(usernameText);
        messageElement.appendChild(usernameElement);
    }

    var textElement = document.createElement('div');
    textElement.innerHTML = marked.parse( message.content);
    messageElement.appendChild(textElement);

    messageArea.appendChild(messageElement);
    messageArea.scrollTop = messageArea.scrollHeight;
}


function getAvatarColor(messageSender) {
    var hash = 0;
    for (var i = 0; i < messageSender.length; i++) {
        hash = 31 * hash + messageSender.charCodeAt(i);
    }

    var index = Math.abs(hash % colors.length);
    return colors[index];
}
function makeSynchronousRequest(url, method, data) {
    var xhr = new XMLHttpRequest();
    xhr.open(method, url, false); // `false` makes the request synchronous

    xhr.setRequestHeader('Content-Type', 'text/html'); // Set headers if needed

    xhr.send(data); // Send the request with data

    if (xhr.status === 200) {
        return xhr.responseText; // Return the response text
    } else {
        throw new Error('Request failed. Status: ' + xhr.status);
    }
}

usernameForm.addEventListener('submit', connect, true)
messageForm.addEventListener('submit', send, true)