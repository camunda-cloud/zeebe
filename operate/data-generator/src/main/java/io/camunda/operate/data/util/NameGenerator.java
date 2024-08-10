/*
 * Copyright Camunda Services GmbH and/or licensed to Camunda Services GmbH under
 * one or more contributor license agreements. See the NOTICE file distributed
 * with this work for additional information regarding copyright ownership.
 * Licensed under the Camunda License 1.0. You may not use this file
 * except in compliance with the Camunda License 1.0.
 */
package io.camunda.operate.data.util;

import java.util.Random;

public abstract class NameGenerator {

  private static String[] firstNames = {
    "James",
    "John",
    "Robert",
    "Michael",
    "William",
    "David",
    "Richard",
    "Joseph",
    "Thomas",
    "Charles",
    "Christopher",
    "Daniel",
    "Matthew",
    "Anthony",
    "Donald",
    "Mark",
    "Paul",
    "Steven",
    "Andrew",
    "Kenneth",
    "Mary",
    "Patricia",
    "Jennifer",
    "Linda",
    "Elizabeth",
    "Barbara",
    "Susan",
    "Jessica",
    "Sarah",
    "Margaret",
    "Karen",
    "Nancy",
    "Lisa",
    "Betty",
    "Dorothy",
    "Sandra",
    "Ashley",
    "Kimberly",
    "Donna",
    "Emily"
  };
  private static String[] lastNames = {
    "Smith",
    "Johnson",
    "Williams",
    "Jones",
    "Brown",
    "Davis",
    "Miller",
    "Wilson",
    "Moore",
    "Taylor",
    "Anderson",
    "Thomas",
    "Jackson",
    "White",
    "Harris",
    "Martin",
    "Thompson",
    "Garcia",
    "Martinez",
    "Robinson",
    "Clark",
    "Rodriguez",
    "Lewis",
    "Lee",
    "Walker",
    "Hall",
    "Allen",
    "Young",
    "Hernandez",
    "King",
    "Wright",
    "Lopez",
    "Hill",
    "Scott",
    "Green",
    "Adams",
    "Baker",
    "Gonzalez",
    "Nelson",
    "Carter"
  };

  private static Random random = new Random();

  public static String getRandomFirstName() {
    return firstNames[random.nextInt(firstNames.length)];
  }

  public static String getRandomLastName() {
    return lastNames[random.nextInt(lastNames.length)];
  }
}
