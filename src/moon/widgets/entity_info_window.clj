(ns ^:no-doc moon.widgets.entity-info-window
  (:require [moon.component :as component :refer [defc]]
            [moon.info :as info]
            [moon.graphics :as g]
            [gdl.ui :as ui]
            [moon.world :refer [mouseover-entity]]))

(def ^:private disallowed-keys [:entity/skills
                                :entity/state
                                :entity/faction
                                :active-skill])

(defc :widgets/entity-info-window
  (component/create [_]
    (let [label (ui/label "")
          window (ui/window {:title "Info"
                             :id :entity-info-window
                             :visible? false
                             :position [(g/gui-viewport-width) 0]
                             :rows [[{:actor label :expand? true}]]})]
      ; TODO do not change window size ... -> no need to invalidate layout, set the whole stage up again
      ; => fix size somehow.
      (ui/add-actor! window (ui/actor {:act (fn update-label-text []
                                              ; items then have 2x pretty-name
                                              #_(.setText (.getTitleLabel window)
                                                          (if-let [entity (mouseover-entity)]
                                                            (info/->text [:property/pretty-name (:property/pretty-name entity)])
                                                            "Entity Info"))
                                              (.setText label
                                                        (str (when-let [entity (mouseover-entity)]
                                                               (info/->text
                                                                ; don't use select-keys as it loses Entity record type
                                                                (apply dissoc entity disallowed-keys)))))
                                              (.pack window))}))
      window)))
