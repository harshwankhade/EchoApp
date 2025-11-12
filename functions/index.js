/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */
'use strict'
const functions = require('firebase-functions');
const admin = require('firebase-admin');

admin.initializeApp();

// Create and deploy your first functions
// https://firebase.google.com/docs/functions/get-started

exports.sendNotification = functions.database.ref('/Notification/{receiver_user_id}/{notification_id}')
	.onWrite((change, context) => {
		const receiver_user_id = context.params.receiver_user_id;
		const notification_id = context.params.notification_id;

		console.log('We have a notification to send to:', receiver_user_id);

		// If notification was deleted, exit
		if (!change.after.val()) {
			console.log('A notification has been deleted:', notification_id);
			return null;
		}

		// Get the notification data
		const notificationData = change.after.val();
		const from_sender_user_id = notificationData.from;

		console.log('You have a notification from:', from_sender_user_id);

		// Get sender's name
		return admin.database().ref(`/Users/${from_sender_user_id}/name`).once('value')
			.then(senderNameSnapshot => {
				const senderUserName = senderNameSnapshot.val() || 'Someone';

				// Get device token for receiver
				return admin.database().ref(`/Users/${receiver_user_id}/device_token`).once('value')
					.then(tokenSnapshot => {
						const token_id = tokenSnapshot.val();

						if (!token_id) {
							console.log('No device token found for user:', receiver_user_id);
							return null;
						}

						const payload = {
							notification: {
								title: "New Chat Request",
								body: `${senderUserName} wants to connect with you.`,
								icon: "default"
							}
						};

						return admin.messaging().sendToDevice(token_id, payload)
							.then(response => {
								console.log('Notification sent successfully.');
								return response;
							})
							.catch(error => {
								console.error('Error sending notification:', error);
								return null;
							});
					});
			})
			.catch(error => {
				console.error('Error processing notification:', error);
				return null;
			});
	});

