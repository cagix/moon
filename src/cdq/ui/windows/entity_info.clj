(ns cdq.ui.windows.entity-info
  (:require [clojure.gdx.scenes.scene2d.group :as group]
            [cdq.ui.label :as label]
            [cdq.ui.window :as window]))

(defn create [{:keys [ctx/ui-viewport]}
              {:keys [y
                      ->label-text]}]
  (let [position [(:viewport/width ui-viewport)
                  y]
        label (label/create {:label/text ""})
        window (window/create {:title "Info"
                               :id :entity-info-window
                               :visible? false
                               :position position
                               :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add! window {:actor/type :actor.type/actor
                        :act (fn [_this _delta {:keys [ctx/mouseover-eid]
                                                :as ctx}]
                               (.setText label (str (if-let [eid mouseover-eid]
                                                      (->label-text @eid ctx)
                                                      "")))
                               (.pack window))})
    window))
