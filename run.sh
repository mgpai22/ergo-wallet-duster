#!/bin/sh
if [ -e serviceOwner.json ]; then
    echo "serviceOwner.json exists, proceeding to run the application..."
    sbt "runMain app.Main"
else
    echo "Error: serviceOwner.json file not found. Please make sure the file exists and try again."
fi