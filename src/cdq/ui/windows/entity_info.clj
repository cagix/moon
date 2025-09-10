(ns cdq.ui.windows.entity-info
  (:require [cdq.stage :as stage]
            [clojure.gdx.scenes.scene2d.group :as group]
            [clojure.vis-ui.widget :as widget]))

(defn create [{:keys [ctx/stage]}
              {:keys [y
                      ->label-text]}]
  (let [position [(stage/viewport-width stage)
                  y]
        label (widget/label {:label/text ""})
        window (widget/window {:title "Info"
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
