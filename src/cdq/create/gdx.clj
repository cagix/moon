(ns cdq.create.gdx
  (:require [cdq.g :as g])
  (:import (com.badlogic.gdx Gdx
                             Input$Keys
                             Input$Buttons)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.utils ScreenUtils)))

(defn add-gdx! [ctx]
  ctx)

(defn- button->code [button]
  (case button
    :left Input$Buttons/LEFT
    ))

(defn- k->code [key]
  (case key
    :minus  Input$Keys/MINUS
    :equals Input$Keys/EQUALS
    :space  Input$Keys/SPACE
    :p      Input$Keys/P
    :enter  Input$Keys/ENTER
    :escape Input$Keys/ESCAPE
    :i      Input$Keys/I
    :e      Input$Keys/E
    :d      Input$Keys/D
    :a      Input$Keys/A
    :w      Input$Keys/W
    :s      Input$Keys/S
    ))

(extend-type gdl.application.Context
  g/Input
  (button-just-pressed? [_ button]
    (.isButtonJustPressed Gdx/input (button->code button)))

  (key-pressed? [_ key]
    (.isKeyPressed Gdx/input (k->code key)))

  (key-just-pressed? [_ key]
    (.isKeyJustPressed Gdx/input (k->code key)))

  g/BaseGraphics
  (delta-time [_]
    (.getDeltaTime Gdx/graphics))

  (set-cursor! [_ cursor]
    (.setCursor Gdx/graphics cursor))

  (frames-per-second [_]
    (.getFramesPerSecond Gdx/graphics))

  (clear-screen! [_]
    (ScreenUtils/clear Color/BLACK)))
