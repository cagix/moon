(ns gdl.create.input
  (:require [clojure.gdx.interop :as interop]
            [gdl.input]))

(defn do! [{:keys [ctx/gdx]} _params]
  (let [this (:input gdx)]
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
