(ns gdl.create.input
  (:require [clojure.gdx.interop :as interop]
            [gdl.input])
  (:import (com.badlogic.gdx Gdx)))

(defn do! [_ctx _params]
  (let [this Gdx/input]
    (reify gdl.input/Input
      (button-just-pressed? [_ button]
        (.isButtonJustPressed this (interop/k->input-button button)))

      (key-pressed? [_ key]
        (.isKeyPressed this (interop/k->input-key key)))

      (key-just-pressed? [_ key]
        (.isKeyJustPressed this (interop/k->input-key key)))

      (mouse-position [_]
        [(.getX this)
         (.getY this)]))))
