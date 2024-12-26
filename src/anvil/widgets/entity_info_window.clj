(ns anvil.widgets.entity-info-window
  (:require [anvil.info :as info]
            [anvil.widgets :as widgets]
            [cdq.context :as world]
            [anvil.app :as app]
            [gdl.context :as ctx]
            [gdl.ui :as ui :refer [ui-actor]]
            [gdl.ui.group :as group]))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text [c]
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [entity (world/mouseover-entity c)]
                (info/text c [:property/pretty-name (:property/pretty-name entity)])
                "Entity Info"))
  (when-let [entity (world/mouseover-entity c)]
    (info/text c ; don't use select-keys as it loses Entity record type
               (apply dissoc entity disallowed-keys))))

(defn-impl widgets/entity-info-window [{:keys [gdl.context/viewport] :as c}]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position [(:width viewport) 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add-actor! window (ui-actor {:act (fn []
                                               (.setText label (str (->label-text @app/state)))
                                               (.pack window))}))
    window))
