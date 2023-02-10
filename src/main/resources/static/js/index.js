/*
 * (C) Copyright 2014 Kurento (http://kurento.org/)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
var axios;
var video;
var webRtcPeer;

window.onload = function() {
	console = new Console();
	video = document.getElementById('video');
	disableStopLeaveButton();
}

window.onbeforeunload = function() {
	ws.close();
}
function messageListender(ws) {
	if (ws) {
		ws.onmessage = function(message) {
			var parsedMessage = JSON.parse(message.data);
			console.info('Received message: ' + message.data);

			switch (parsedMessage.id) {
				case 'presenterResponse':
					presenterResponse(parsedMessage);
					break;
				case 'viewerResponse':
					viewerResponse(parsedMessage);
					break;
				case 'iceCandidate':
					// Exchanged ICE candidates between both peer, by sending the ones generated in the browser,
					// and processing the candidates received by the remote peer.
					webRtcPeer.addIceCandidate(parsedMessage.candidate, function(error) {
						if (error)
							return console.error('Error adding candidate: ' + error);
					});
					break;
				case 'stopCommunication':
					dispose();
					break;
				default:
					console.error('Unrecognized message', parsedMessage);
			}
		}
	}
}

// Received an SDP answer from the remote peer, and have the webRtcPeer process that answer.
function presenterResponse(message) {
	if (message.response != 'accepted') {
		var errorMsg = message.message ? message.message : 'Unknow error';
		console.info('Call not accepted for the following reason: ' + errorMsg);
		dispose();
	} else {
		webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
	}
}

function viewerResponse(message) {
	if (message.response != 'accepted') {
		var errorMsg = message.message ? message.message : 'Unknow error';
		console.info('Call not accepted for the following reason: ' + errorMsg);
		dispose();
	} else {
		webRtcPeer.processAnswer(message.sdpAnswer, function(error) {
			if (error)
				return console.error(error);
		});
	}
}

async function presenter() {
	// notice: test
	var userResponse = await axios.post("/signal/room/", {
	}, {
		headers: {
			email: "presenter@naver.com"
		}
	});
	if (userResponse.status !== 201) {
		return;
	}
	ws = new WebSocket('ws://' + location.host + '/signal/ws');
	console.log(location.host);
	if (!webRtcPeer) {
		showSpinner(video);
		var options = {
			localVideo : video,
			onicecandidate : onIceCandidate
		}
		webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerSendonly(options,
				function(error) {
					if (error) {
						return console.error(error);
					}
					webRtcPeer.generateOffer(onOfferPresenter);
				});
		enableStopLeaveButton();
	}
	messageListender(ws);
}

async function viewer() {
	var joinResponse = await axios.post("/signal/room/view", {
	}, {
		headers: {
			email: "viewer@naver.com", // new viewer
			roomId: "presenter@naver.com"
		}
	});
	console.log(joinResponse); // 201

	ws = new WebSocket('ws://' + location.host + '/signal/ws');
	console.log(location.host);
	if (!webRtcPeer) {
		showSpinner(video);
		var options = {
			remoteVideo : video,
			onicecandidate : onIceCandidate
		}
		webRtcPeer = new kurentoUtils.WebRtcPeer.WebRtcPeerRecvonly(options,
			function(error) {
				if (error) {
					return console.error(error);
				}
				this.generateOffer(onOfferViewer);
			});
		enableStopLeaveButton();
	}
	messageListender(ws);
}

async function stop() {
	var stopResponse = await axios.delete("/signal/room", {
		headers: {
			email: "presenter@naver.com",
			roomId: "presenter@naver.com"
		}
	});
	console.log(stopResponse);
	dispose();
}

async function leave() {
	var leaveResponse = await axios.delete("/signal/room/view", {
		headers: {
			email: "viewer@naver.com",
			roomId: "presenter@naver.com"
		}
	});
	console.log(leaveResponse);
	dispose();
}

function onOfferPresenter(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	// 이 메세지 형식으로 iOS에서 보내주도록 수정완료
	var message = {
		id : 'presenter',
		email : 'presenter@naver.com', // notice: test
		sdpOffer : offerSdp
	}
	sendMessage(message);
}


function onOfferViewer(error, offerSdp) {
	if (error)
		return console.error('Error generating the offer');
	console.info('Invoking SDP offer callback function ' + location.host);
	var message = {
		id : 'viewer',
		email: 'viewer@naver.com',
		sdpOffer : offerSdp,
	}
	console.info(' ------------------------------------- ');
	console.info(message);
	sendMessage(message);
}

function onIceCandidate(candidate) {
	console.log("Local candidate" + JSON.stringify(candidate));
	var message = {
		id : 'onIceCandidate',
		roomId : 'presenter@naver.com', // notice: test
		candidate : candidate
	};
	sendMessage(message);
}


function dispose() {
	if (webRtcPeer) {
		webRtcPeer.dispose();
		webRtcPeer = null;
	}
	hideSpinner(video);

	disableStopLeaveButton();
}

function disableStopLeaveButton() {
	enableButton('#presenter', 'presenter()');
	enableButton('#viewer', 'viewer()');
	disableButton('#stop');
	disableButton('#leave')
}

function enableStopLeaveButton() {
	disableButton('#presenter');
	disableButton('#viewer');
	enableButton('#stop', 'stop()');
	enableButton('#leave', 'leave()')
}

function disableButton(id) {
	$(id).attr('disabled', true);
	$(id).removeAttr('onclick');
}

function enableButton(id, functionName) {
	$(id).attr('disabled', false);
	$(id).attr('onclick', functionName);
}

function sendMessage(message) {
	var jsonMessage = JSON.stringify(message);
	console.log('Sending message: ' + jsonMessage);
	ws.send(jsonMessage);
}

function showSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].poster = './img/transparent-1px.png';
		arguments[i].style.background = 'center transparent url("./img/spinner.gif") no-repeat';
	}
}

function hideSpinner() {
	for (var i = 0; i < arguments.length; i++) {
		arguments[i].src = '';
		arguments[i].poster = './img/webrtc.png';
		arguments[i].style.background = '';
	}
}

/**
 * Lightbox utility (to display media pipeline image in a modal dialog)
 */
$(document).delegate('*[data-toggle="lightbox"]', 'click', function(event) {
	event.preventDefault();
	$(this).ekkoLightbox();
});
