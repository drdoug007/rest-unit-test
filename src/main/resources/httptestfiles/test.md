# Test Report: httptestfiles/cardealer.http

## GET request to example server

**Request:** `GET http://127.0.0.1:8080/api/cardealer`

**Response Status:** 200

**SQL Query executed.** Returned 4 rows.

**Test Results:**

- ✅ Request executed successfully
- ✅ Response includes more than 1 car dealer
- ✅ SQL query returned results
- ✅ Response Data Matches Database

### Car Dealers

Environment: Development

| name              |
|-------------------|
| Luxury Motors     |
| Reliable Rides    |
| Speedway Autos    |
| Eco Friendly Cars |

### Response Message

```json
[
  {
    "id": 1,
    "name": "Luxury Motors",
    "carList": [
      {
        "id": 1,
        "make": "Volvo",
        "model": "Silverado",
        "color": "Silver",
        "price": "40286 USD"
      },
      {
        "id": 2,
        "make": "Ford",
        "model": "Sportage",
        "color": "White",
        "price": "19408 USD"
      },
      {
        "id": 3,
        "make": "Volkswagen",
        "model": "Silverado",
        "color": "Red",
        "price": "21243 USD"
      },
      {
        "id": 4,
        "make": "Volkswagen",
        "model": "Altima",
        "color": "Silver",
        "price": "50717 USD"
      },
      {
        "id": 5,
        "make": "Kia",
        "model": "Golf",
        "color": "Black",
        "price": "36463 USD"
      },
      {
        "id": 6,
        "make": "Nissan",
        "model": "Model 3",
        "color": "Red",
        "price": "50785 USD"
      },
      {
        "id": 7,
        "make": "Volvo",
        "model": "Silverado",
        "color": "White",
        "price": "40321 USD"
      },
      {
        "id": 8,
        "make": "Ford",
        "model": "Civic",
        "color": "White",
        "price": "51704 USD"
      },
      {
        "id": 9,
        "make": "Toyota",
        "model": "XC60",
        "color": "Red",
        "price": "37065 USD"
      },
      {
        "id": 10,
        "make": "Tesla",
        "model": "Golf",
        "color": "Silver",
        "price": "40119 USD"
      },
      {
        "id": 11,
        "make": "Ford",
        "model": "Elantra",
        "color": "Silver",
        "price": "41746 USD"
      },
      {
        "id": 12,
        "make": "Nissan",
        "model": "Model 3",
        "color": "Black",
        "price": "27357 USD"
      },
      {
        "id": 13,
        "make": "Volkswagen",
        "model": "Corolla",
        "color": "Red",
        "price": "21007 USD"
      },
      {
        "id": 14,
        "make": "Honda",
        "model": "Model 3",
        "color": "Grey",
        "price": "22067 USD"
      },
      {
        "id": 15,
        "make": "BMW",
        "model": "Civic",
        "color": "Black",
        "price": "20687 USD"
      }
    ]
  },
  {
    "id": 2,
    "name": "Reliable Rides",
    "carList": [
      {
        "id": 16,
        "make": "BMW",
        "model": "F-150",
        "color": "Grey",
        "price": "52158 USD"
      },
      {
        "id": 17,
        "make": "Subaru",
        "model": "C-Class",
        "color": "White",
        "price": "59738 USD"
      },
      {
        "id": 18,
        "make": "Kia",
        "model": "Civic",
        "color": "Silver",
        "price": "26338 USD"
      },
      {
        "id": 19,
        "make": "Tesla",
        "model": "CX-5",
        "color": "Silver",
        "price": "52771 USD"
      },
      {
        "id": 20,
        "make": "Tesla",
        "model": "Altima",
        "color": "White",
        "price": "47430 USD"
      },
      {
        "id": 21,
        "make": "Mazda",
        "model": "Altima",
        "color": "Silver",
        "price": "31342 USD"
      },
      {
        "id": 22,
        "make": "Volkswagen",
        "model": "Outback",
        "color": "White",
        "price": "21116 USD"
      },
      {
        "id": 23,
        "make": "Chevrolet",
        "model": "Elantra",
        "color": "White",
        "price": "37577 USD"
      },
      {
        "id": 24,
        "make": "Hyundai",
        "model": "Altima",
        "color": "Black",
        "price": "55743 USD"
      },
      {
        "id": 25,
        "make": "Ford",
        "model": "Outback",
        "color": "White",
        "price": "21252 USD"
      },
      {
        "id": 26,
        "make": "Mercedes",
        "model": "Silverado",
        "color": "Red",
        "price": "59380 USD"
      },
      {
        "id": 27,
        "make": "Nissan",
        "model": "XC60",
        "color": "Grey",
        "price": "21394 USD"
      },
      {
        "id": 28,
        "make": "Ford",
        "model": "Outback",
        "color": "Blue",
        "price": "34283 USD"
      },
      {
        "id": 29,
        "make": "BMW",
        "model": "Corolla",
        "color": "Red",
        "price": "58712 USD"
      },
      {
        "id": 30,
        "make": "Hyundai",
        "model": "XC60",
        "color": "White",
        "price": "26790 USD"
      }
    ]
  },
  {
    "id": 3,
    "name": "Speedway Autos",
    "carList": [
      {
        "id": 31,
        "make": "Ford",
        "model": "Civic",
        "color": "Blue",
        "price": "28859 USD"
      },
      {
        "id": 32,
        "make": "Subaru",
        "model": "Sportage",
        "color": "Silver",
        "price": "29078 USD"
      },
      {
        "id": 33,
        "make": "BMW",
        "model": "A4",
        "color": "Silver",
        "price": "55036 USD"
      },
      {
        "id": 34,
        "make": "Chevrolet",
        "model": "Silverado",
        "color": "Silver",
        "price": "40323 USD"
      },
      {
        "id": 35,
        "make": "BMW",
        "model": "Civic",
        "color": "Red",
        "price": "16828 USD"
      },
      {
        "id": 36,
        "make": "Chevrolet",
        "model": "Altima",
        "color": "Silver",
        "price": "34666 USD"
      },
      {
        "id": 37,
        "make": "Ford",
        "model": "Elantra",
        "color": "Black",
        "price": "30154 USD"
      },
      {
        "id": 38,
        "make": "Kia",
        "model": "Corolla",
        "color": "Black",
        "price": "45614 USD"
      },
      {
        "id": 39,
        "make": "Volvo",
        "model": "Corolla",
        "color": "Silver",
        "price": "45337 USD"
      },
      {
        "id": 40,
        "make": "Toyota",
        "model": "Corolla",
        "color": "White",
        "price": "31585 USD"
      },
      {
        "id": 41,
        "make": "Ford",
        "model": "Silverado",
        "color": "Black",
        "price": "35484 USD"
      },
      {
        "id": 42,
        "make": "Mercedes",
        "model": "CX-5",
        "color": "Red",
        "price": "55489 USD"
      },
      {
        "id": 43,
        "make": "Tesla",
        "model": "CX-5",
        "color": "Grey",
        "price": "37021 USD"
      },
      {
        "id": 44,
        "make": "Tesla",
        "model": "F-150",
        "color": "Blue",
        "price": "37814 USD"
      },
      {
        "id": 45,
        "make": "Mazda",
        "model": "3 Series",
        "color": "Black",
        "price": "25871 USD"
      }
    ]
  },
  {
    "id": 4,
    "name": "Eco Friendly Cars",
    "carList": [
      {
        "id": 46,
        "make": "Nissan",
        "model": "3 Series",
        "color": "White",
        "price": "26616 USD"
      },
      {
        "id": 47,
        "make": "BMW",
        "model": "Elantra",
        "color": "Grey",
        "price": "18697 USD"
      },
      {
        "id": 48,
        "make": "Mercedes",
        "model": "Civic",
        "color": "Black",
        "price": "31211 USD"
      },
      {
        "id": 49,
        "make": "Toyota",
        "model": "F-150",
        "color": "Red",
        "price": "23027 USD"
      },
      {
        "id": 50,
        "make": "Mercedes",
        "model": "Altima",
        "color": "Grey",
        "price": "40628 USD"
      },
      {
        "id": 51,
        "make": "Nissan",
        "model": "XC60",
        "color": "Blue",
        "price": "33871 USD"
      },
      {
        "id": 52,
        "make": "Tesla",
        "model": "C-Class",
        "color": "Silver",
        "price": "52930 USD"
      },
      {
        "id": 53,
        "make": "Subaru",
        "model": "Model 3",
        "color": "Grey",
        "price": "15339 USD"
      },
      {
        "id": 54,
        "make": "BMW",
        "model": "Silverado",
        "color": "White",
        "price": "28063 USD"
      },
      {
        "id": 55,
        "make": "Chevrolet",
        "model": "Civic",
        "color": "Black",
        "price": "25860 USD"
      },
      {
        "id": 56,
        "make": "Volkswagen",
        "model": "XC60",
        "color": "Grey",
        "price": "50279 USD"
      },
      {
        "id": 57,
        "make": "Honda",
        "model": "Elantra",
        "color": "Blue",
        "price": "24898 USD"
      },
      {
        "id": 58,
        "make": "Volkswagen",
        "model": "3 Series",
        "color": "Grey",
        "price": "52486 USD"
      },
      {
        "id": 59,
        "make": "Chevrolet",
        "model": "Model 3",
        "color": "Black",
        "price": "38609 USD"
      },
      {
        "id": 60,
        "make": "Subaru",
        "model": "Elantra",
        "color": "Blue",
        "price": "28387 USD"
      }
    ]
  }
]
```
