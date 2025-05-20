(ns cdq.ui.entity-info
  (:require [cdq.ctx :as ctx]
            [cdq.info :as info]
            [gdl.ui :as ui]))

(comment

 ; items then have 2x pretty-name
 #_(.setText (.getTitleLabel window)
             (info/text [:property/pretty-name (:property/pretty-name entity)])
             "Entity Info")
 )

(def disallowed-keys [:entity/skills
                      #_:entity/fsm
                      :entity/faction
                      :active-skill])

(defn- ->label-text [entity]
  ; don't use select-keys as it loses Entity record type
  (info/text (apply dissoc entity disallowed-keys)))

(defn create [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.addActor window (ui/actor {:act (fn [_this _delta]
                                        (.setText label (str (if-let [eid ctx/mousover-eid]
                                                               (->label-text @eid)
                                                               "")))
                                        (.pack window))}))
    window))
