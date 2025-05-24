(ns cdq.ui.entity-info
  (:require [cdq.g :as g]
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

; TODO details how the text looks move to info
; only for :
; * skill
; * entity -> all sub-types
; * item
; => can test separately !?

(defn- ->label-text [entity ctx]
  ; don't use select-keys as it loses Entity record type
  (g/info-text ctx (apply dissoc entity disallowed-keys)))

(defn create [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.addActor window (ui/actor {:act (fn [_this _delta {:keys [ctx/mouseover-eid]
                                                         :as ctx}]
                                        (.setText label (str (if-let [eid mouseover-eid]
                                                               (->label-text @eid ctx)
                                                               "")))
                                        (.pack window))}))
    window))
