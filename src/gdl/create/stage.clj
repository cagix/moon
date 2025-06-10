(ns gdl.create.stage
  (:require [gdl.ui :as ui]
            [gdl.ui.stage :as stage])
  (:import (com.badlogic.gdx Gdx)))

(defn do! [{:keys [ctx/graphics]} config]
  (ui/load! config)
  (let [stage (ui/stage (:java-object (:ui-viewport graphics))
                        (:batch graphics))]
    (.setInputProcessor Gdx/input stage)
    (reify
      ; TODO is disposable but not sure if needed as we handle batch ourself.
      clojure.lang.ILookup
      (valAt [_ key]
        (key stage))

      stage/Stage
      (render! [_ ctx]
        (ui/act! stage ctx)
        (ui/draw! stage ctx)
        ctx)

      (add! [_ actor] ; -> re-use gdl.ui/add! ?
        (ui/add! stage actor))

      (clear! [_]
        (ui/clear! stage))

      (hit [_ position]
        (ui/hit stage position))

      (find-actor [_ actor-name]
        (-> stage
            ui/root
            (ui/find-actor actor-name))))))
