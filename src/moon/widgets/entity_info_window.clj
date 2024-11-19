(ns moon.widgets.entity-info-window
  (:require [gdl.info :as info]
            [gdl.ui :as ui]
            [moon.core :refer [gui-viewport-width]]
            [moon.world.mouseover :as mouseover]))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn create []
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position [(gui-viewport-width) 0]
                           :rows [[{:actor label :expand? true}]]})]
    ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (ui/add-actor! window (ui/actor {:act (fn update-label-text []
                                            ; items then have 2x pretty-name
                                            #_(.setText (.getTitleLabel window)
                                                        (if-let [entity (mouseover/entity)]
                                                          (info/text [:property/pretty-name (:property/pretty-name entity)])
                                                          "Entity Info"))
                                            (.setText label
                                                      (str (when-let [entity (mouseover/entity)]
                                                             (info/text
                                                              ; don't use select-keys as it loses Entity record type
                                                              (apply dissoc entity disallowed-keys)))))
                                            (.pack window))}))
    window))
