@echo off
echo Generating API documentation...
echo This requires the service to be running

REM clear old docs
del api.md

set port=3001
set title=Media Service API

REM install graphql-markdown if not installed
if not exist node_modules\graphql-markdown (
   echo Installing graphql-markdown, run this script again after installation
   npm install graphql-markdown
)

npx graphql-markdown "http://localhost:%port%/graphql" --title "%title%" > api.md