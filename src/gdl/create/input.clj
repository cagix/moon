(ns gdl.create.input
  (:require [clojure.gdx.interop :as interop]
            [gdl.input :as input])
  (:import (com.badlogic.gdx Gdx)))

(defn do! [ctx]
  (assoc ctx :ctx/input (let [this Gdx/input]
                          (reify input/Input
                            (button-just-pressed? [_ button]
                              (.isButtonJustPressed this (interop/k->input-button button)))

                            (key-pressed? [_ key]
                              (.isKeyPressed this (interop/k->input-key key)))

                            (key-just-pressed? [_ key]
                              (.isKeyJustPressed this (interop/k->input-key key)))

                            (set-processor! [_ input-processor]
                              (.setInputProcessor this input-processor))

                            (mouse-position [_]
                              [(.getX this)
                               (.getY this)])))))
