(ns cdq.ui.windows.entity-info
  (:require [cdq.ctx.stage :as stage]
            [clojure.scene2d :as scene2d]
            [clojure.scene2d.group :as group]))

(defn create [{:keys [ctx/stage]}
              {:keys [y
                      ->label-text]}]
  (let [position [(stage/viewport-width stage)
                  y]
        label (scene2d/build {:actor/type :actor.type/label
                              :label/text ""})
        window (scene2d/build {:actor/type :actor.type/window
                               :title "Info"
                               :actor/name "cdq.ui.windows.entity-info"
                               :actor/visible? false
                               :actor/position position
                               :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add! window (scene2d/build
                        {:actor/type :actor.type/actor
                         :act (fn [_this _delta {:keys [ctx/world]
                                                 :as ctx}]
                                (.setText label (str (if-let [eid (:world/mouseover-eid world)]
                                                       (->label-text @eid ctx)
                                                       "")))
                                (.pack window))}))
    window))
