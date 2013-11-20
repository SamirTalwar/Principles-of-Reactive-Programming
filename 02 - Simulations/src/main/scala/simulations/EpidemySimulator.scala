package simulations

import math.random

class EpidemySimulator extends Simulator {

  def randomBelow(i: Int) = (random * i).toInt

  protected[simulations] object SimConfig {
    val roomRows: Int = 8
    val roomColumns: Int = 8

    val population: Int = 300
    val transmissibilityRate = 0.4
    def sickPeopleIn(population: Int) = population / 100

    // to complete: additional parameters of simulation
  }

  import SimConfig._

  val persons: List[Person] = {
    val sickPeopleCount = sickPeopleIn(population)
    val sickPeople = (0 until sickPeopleCount).map(new Person(_)).toList
    sickPeople.foreach(_.infect())
    val healthyPeople = (sickPeopleCount until population).map(new Person(_)).toList
    sickPeople ++ healthyPeople
  }

  class Person (val id: Int) {
    var infected = false
    var sick = false
    var immune = false
    var dead = false

    // demonstrates random number generation
    var row: Int = randomBelow(roomRows)
    var col: Int = randomBelow(roomColumns)

    def wrap(f: (Int, Int) => (Int, Int))(row: Int, col: Int) = {
      val (newRow, newCol) = f(row, col)
      ((newRow + roomRows) % roomRows, (newCol + roomColumns) % roomColumns)
    }

    val Directions = List(
      wrap((r: Int, c: Int) => (r,     c - 1)) _,
      wrap((r: Int, c: Int) => (r + 1, c    )) _,
      wrap((r: Int, c: Int) => (r,     c + 1)) _,
      wrap((r: Int, c: Int) => (r - 1, c    )) _
    )

    def potentiallyInfect() {
      if (random < transmissibilityRate) {
        infect()
      }
    }

    def infect() {
      infected = true
    }

    def act() {
      val possibleMovements = Directions.map(d => d(row, col)).filter { case (newRow, newCol) =>
        persons.filter(p => p.row == newRow && p.col == newCol).forall(p => !p.sick)
      }
      if (!possibleMovements.isEmpty) {
        val movement = possibleMovements(randomBelow(possibleMovements.size))
        row = movement._1
        col = movement._2

        if (persons.exists(_.infected)) {
          potentiallyInfect()
        }
      }
      actSoon()
    }

    def actSoon() = afterDelay(randomBelow(5) + 1) {
      act()
    }

    actSoon()
  }
}
