(ns context.input
  (:require api.context)
  (:import com.badlogic.gdx.Gdx))

(extend-type api.context.Context
  api.context/Input
  (button-pressed?      [_ button] (.isButtonPressed     Gdx/input button))
  (button-just-pressed? [_ button] (.isButtonJustPressed Gdx/input button))

  (key-pressed?      [_ k] (.isKeyPressed     Gdx/input k))
  (key-just-pressed? [_ k] (.isKeyJustPressed Gdx/input k)))