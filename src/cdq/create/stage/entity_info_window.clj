(ns cdq.create.stage.entity-info-window
  (:require [cdq.info :as info]
            [cdq.ui :as ui :refer [ui-actor]]
            [cdq.scene2d.group :as group]))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text [{:keys [cdq.context/mouseover-eid] :as c}]
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [eid mouseover-eid]
                (info/text c [:property/pretty-name (:property/pretty-name @eid)])
                "Entity Info"))
  (when-let [eid mouseover-eid]
    (info/text c ; don't use select-keys as it loses Entity record type
               (apply dissoc @eid disallowed-keys))))

(defn create [{:keys [cdq.graphics/ui-viewport] :as c}]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position [(:width ui-viewport) 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add-actor! window (ui-actor {:act (fn [context]
                                               (.setText label (str (->label-text context)))
                                               (.pack window))}))
    window))
