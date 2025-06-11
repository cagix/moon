(ns gdl.create.stage
  (:require [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]))

(defn do!
  [graphics
   gdl
   config]
  (ui/load! config)
  (let [stage (ui/stage (:java-object (:ui-viewport graphics))
                        (:batch graphics))]
    (input/set-input-processor! gdl stage)
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
