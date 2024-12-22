(ns anvil.world.widgets.entity-info-window
  (:require [anvil.info :as info]
            [anvil.world :as world]
            [anvil.world.widgets :as widgets]
            [gdl.graphics :as g]
            [gdl.ui :as ui :refer [ui-actor]]
            [gdl.ui.group :as group]))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn-impl widgets/entity-info-window []
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position [g/viewport-width 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (group/add-actor! window (ui-actor {:act (fn update-label-text []
                                               ; items then have 2x pretty-name
                                               #_(.setText (.getTitleLabel window)
                                                           (if-let [entity (world/mouseover-entity)]
                                                             (info/text [:property/pretty-name (:property/pretty-name entity)])
                                                             "Entity Info"))
                                               (.setText label
                                                         (str (when-let [entity (world/mouseover-entity)]
                                                                (info/text
                                                                 ; don't use select-keys as it loses Entity record type
                                                                 (apply dissoc entity disallowed-keys)))))
                                               (.pack window))}))
    window))
