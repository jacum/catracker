package nl.pragmasoft.catracker

import cats.effect.IO
import nl.pragmasoft.catracker.Model.PositionRepository
import org.scalamock.matchers.Matchers
import org.scalamock.scalatest.MockFactory
import org.scalatest.wordspec.AnyWordSpec

class ApiSpec extends AnyWordSpec with MockFactory with Matchers {
  private val repository = stub[PositionRepository[IO]]

//  private val service = new ApiHandler[IO](repository).routes

//  "TodoService" should {
//    "create a todo" in {
//      val id = 1
//      val todo = Todo(None, "my todo", Low)
//      (repository.createTodo _).when(todo).returns(IO.pure(todo.copy(id = Some(id))))
//      val createJson =
//        json"""
//        {
//          "description": ${ todo.description },
//          "importance": ${ todo.importance.value }
//        }"""
//      val response = serve(Request[IO](POST, uri"/todos").withEntity(createJson))
//      response.status shouldBe Status.Created
//      response.as[Json].unsafeRunSync() shouldBe
//        json"""
//        {
//          "id": $id,
//          "description": ${ todo.description },
//          "importance": ${ todo.importance.value }
//        }"""
//    }
//
//    "update a todo" in {
//      val id = 1
//      val todo = Todo(None, "updated todo", Medium)
//      (repository.updateTodo _).when(id, todo).returns(IO.pure(Right(todo.copy(id = Some(id)))))
//      val updateJson =
//        json"""
//        {
//          "description": ${ todo.description },
//          "importance": ${ todo.importance.value }
//        }"""
//
//      val response = serve(Request[IO](PUT, Uri.unsafeFromString(s"/todos/$id")).withEntity(updateJson))
//      response.status shouldBe Status.Ok
//      response.as[Json].unsafeRunSync() shouldBe
//        json"""
//        {
//          "id": $id,
//          "description": ${ todo.description },
//          "importance": ${ todo.importance.value }
//        }"""
//    }
//
//    "return a single todo" in {
//      val id = 1
//      val todo = Todo(Some(id), "my todo", High)
//      (repository.getTodo _).when(id).returns(IO.pure(Right(todo)))
//
//      val response = serve(Request[IO](GET, Uri.unsafeFromString(s"/todos/$id")))
//      response.status shouldBe Status.Ok
//      response.as[Json].unsafeRunSync() shouldBe
//        json"""
//        {
//          "id": $id,
//          "description": ${ todo.description },
//          "importance": ${ todo.importance.value }
//        }"""
//    }
//
//    "return all todos" in {
//      val id1 = 1
//      val todo1 = Todo(Some(id1), "my todo 1", High)
//      val id2 = 2
//      val todo2 = Todo(Some(id2), "my todo 2", Medium)
//      val todos = Stream(todo1, todo2)
//      (repository.getTodos _).when().returns(todos)
//
//      val response = serve(Request[IO](GET, uri"/todos"))
//      response.status shouldBe Status.Ok
//      response.as[Json].unsafeRunSync() shouldBe
//        json"""
//        [
//         {
//           "id": $id1,
//           "description": ${ todo1.description },
//           "importance": ${ todo1.importance.value }
//         },
//         {
//           "id": $id2,
//           "description": ${ todo2.description },
//           "importance": ${ todo2.importance.value }
//         }
//        ]"""
//    }
//
//    "delete a todo" in {
//      val id = 1
//      (repository.deleteTodo _).when(id).returns(IO.pure(Right(())))
//
//      val response = serve(Request[IO](DELETE, Uri.unsafeFromString(s"/todos/$id")))
//      response.status shouldBe Status.NoContent
//    }
//  }
//
//  private def serve(request: Request[IO]): Response[IO] = {
//    service.orNotFound(request).unsafeRunSync()
//  }
}
