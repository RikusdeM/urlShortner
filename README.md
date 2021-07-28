# Coding Challenge
URL shortener HTTP service.

## Problem Specification
Design and implement an URL shortener HTTP service that fulfills the following criteria:

* Provides an HTTP API to:
	* Shorten a URL
	* Redirect to the long URL from the shortened URL
* Shortened URL requirements:
	* The ID of the shortened URL needs to be unique (across past and concurrent requests)
	* The ID of the shortened URL should be as short as possible (max. 8 characters long)
	* The long/shortened URL mapping needs to be persisted and shouldn't be lost after a backend service restart

Which technologies you use and how you solve this problem is completely up to you.

## Assessment Criteria
We expect well-structured, clean code without needless duplication. The solution should be automatically verified and follow best practices.

What we will look at:

* How easy it is to understand and maintain your code.
* How you verify your software, whether by automated tests or otherwise.
* How clean your design and implementation is.

And of course you don’t have to over do it. In the end, we know that you have to work on it next to your daily life. A simple solution is perfectly fine. Should you hit a road block don’t hesitate to ask us.
