(ns moon.widgets.windows
  (:require [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [moon.controls :as controls]
            [moon.component :as component]
            [moon.stage :as stage]))

(defn create []
  (ui/group {:id :windows
             :actors [(component/create [:widgets/entity-info-window])
                      (component/create [:widgets/inventory])]}))

(defn- windows []
  (:windows (stage/get)))

(defn inventory []
  (get (windows) :inventory-window))

(defn check-hotkeys []
  (doseq [window-id [:inventory-window :entity-info-window]
          :when (controls/toggle-visible? window-id)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn close-all []
  (let [windows (ui/children (windows))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible! % false) windows))))


