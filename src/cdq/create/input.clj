(ns cdq.create.input
  (:require [clojure.gdx.interop :as interop]
            [gdl.input])
  (:import (com.badlogic.gdx Gdx
                             Input)))

(defn- make-input [^Input input]
  (reify gdl.input/Input
    (button-just-pressed? [_ button]
      (.isButtonJustPressed input (interop/k->input-button button)))

    (key-pressed? [_ key]
      (.isKeyPressed input (interop/k->input-key key)))

    (key-just-pressed? [_ key]
      (.isKeyJustPressed input (interop/k->input-key key)))

    (set-processor! [_ input-processor]
      (.setInputProcessor input input-processor))

    (mouse-position [_]
      [(.getX input)
       (.getY input)])))

(defn do! [ctx]
  (assoc ctx :ctx/input (make-input Gdx/input)))
