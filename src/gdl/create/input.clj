(ns gdl.create.input
  (:require [clojure.gdx.input :as input]
            [clojure.gdx.input.buttons :as input.buttons]
            [clojure.gdx.input.keys :as input.keys]
            [gdl.input]))

(defn create-input [gdx-input]
  (reify gdl.input/Input
    (button-just-pressed? [_ button]
      (input/button-just-pressed? gdx-input (input.buttons/->from-k button)))

    (key-pressed? [_ key]
      (input/key-pressed? gdx-input (input.keys/->from-k key)))

    (key-just-pressed? [_ key]
      (input/key-just-pressed? gdx-input (input.keys/->from-k key)))

    (mouse-position [_]
      [(input/x gdx-input)
       (input/y gdx-input)])))
