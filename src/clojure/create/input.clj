(ns clojure.create.input
  (:require [clojure.gdx :as gdx]
            [clojure.input :as input])
  (:import (com.badlogic.gdx Gdx)))

(defn do! [ctx]
  (assoc ctx :ctx/input (let [this Gdx/input]
                          (reify input/Input
                            (button-just-pressed? [_ button]
                              (.isButtonJustPressed this (gdx/k->input-button button)))

                            (key-pressed? [_ key]
                              (.isKeyPressed this (gdx/k->input-key key)))

                            (key-just-pressed? [_ key]
                              (.isKeyJustPressed this (gdx/k->input-key key)))

                            (set-processor! [_ input-processor]
                              (.setInputProcessor this input-processor))

                            (mouse-position [_]
                              [(.getX this)
                               (.getY this)])))))
