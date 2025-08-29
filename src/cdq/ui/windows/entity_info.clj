(ns cdq.ui.windows.entity-info
  (:require [cdq.ui.group :as group]
            [gdx.ui :as ui]))

(defn create [{:keys [ctx/graphics]}
              {:keys [y
                      ->label-text]}]
  (let [position [(:viewport/width (:ui-viewport graphics))
                  y]
        label (ui/label {:label/text ""})
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add! window {:actor/type :actor.type/actor
                        :act (fn [_this _delta {:keys [ctx/world]
                                                :as ctx}]
                               (.setText label (str (if-let [eid (:world/mouseover-eid world)]
                                                      (->label-text @eid ctx)
                                                      "")))
                               (.pack window))})
    window))
