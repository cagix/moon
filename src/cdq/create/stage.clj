(ns cdq.create.stage
  (:require [cdq.input :as input]
            [gdl.ui :as ui]
            [gdl.ui.stage :as stage]))

; * find by name string ( TEST msg )

; => then we can remove stage from gdl.ui ????

; also outdated context -> pass directly atom state?
; swap! at each render?

(defn do! [{:keys [ctx/ui-viewport
                   ctx/batch
                   ctx/config]
            :as ctx}]
  (ui/load! (:ui config))
  (let [stage (ui/stage (:java-object ui-viewport)
                        batch)]
    (input/set-processor! ctx stage)
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
