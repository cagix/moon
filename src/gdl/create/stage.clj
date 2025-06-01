(ns gdl.create.stage
  (:require [gdl.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]))

; FIXME also outdated context at input-processors outside of stage/render!
; -> pass directly atom state to stage?
; swap! at each render?

(defn do! [{:keys [ctx/batch
                   ctx/config
                   ctx/input
                   ctx/ui-viewport]
            :as ctx}]
  (ui/load! (:ui config))
  (let [stage (ui/stage (:java-object ui-viewport)
                        batch)]
    (input/set-processor! input stage)
    (assoc ctx :ctx/stage (reify
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
                                  (ui/find-actor actor-name)))))))
