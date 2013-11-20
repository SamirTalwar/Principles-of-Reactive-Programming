package simulations

import math.random

class EpidemySimulator extends Simulator {

  def randomBelow(i: Int) = (random * i).toInt

  protected[simulations] object SimConfig {
    val population: Int = 300
    val roomRows: Int = 8
    val roomColumns: Int = 8

    // to complete: additional parameters of simulation
  }

  import SimConfig._

  val persons: List[Person] = (0 until population).map(new Person(_)).toList

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

    def act() {
      val possibleMovements = Directions.map(d => d(row, col)).filter { case (newRow, newCol) =>
        persons.filter(p => p.row == newRow && p.col == newCol).forall(p => !p.sick)
      }
      if (!possibleMovements.isEmpty) {
        val movement = possibleMovements(randomBelow(possibleMovements.size))
        row = movement._1
        col = movement._2
      }
      actSoon()
    }

    def actSoon() = afterDelay(randomBelow(5) + 1) {
      act()
    }

    actSoon()
  }
}
