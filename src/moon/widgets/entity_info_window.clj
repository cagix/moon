(ns moon.widgets.entity-info-window
  (:require [gdl.graphics.gui-view :as gui-view]
            [gdl.ui :as ui]
            [moon.component :as component]
            [moon.world.mouseover :as mouseover]))

(def ^:private disallowed-keys [:entity/skills
                                :entity/fsm
                                :entity/faction
                                :active-skill])

(defmethods :widgets/entity-info-window
  (component/create [_]
    (let [label (ui/label "")
          window (ui/window {:title "Info"
                             :id :entity-info-window
                             :visible? false
                             :position [(gui-view/width) 0]
                             :rows [[{:actor label :expand? true}]]})]
      ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
      ; => fix size somehow.
      (ui/add-actor! window (ui/actor {:act (fn update-label-text []
                                              ; items then have 2x pretty-name
                                              #_(.setText (.getTitleLabel window)
                                                          (if-let [entity (mouseover/entity)]
                                                            (component/->info [:property/pretty-name (:property/pretty-name entity)])
                                                            "Entity Info"))
                                              (.setText label
                                                        (str (when-let [entity (mouseover/entity)]
                                                               (component/->info
                                                                ; don't use select-keys as it loses Entity record type
                                                                (apply dissoc entity disallowed-keys)))))
                                              (.pack window))}))
      window)))
