(ns moon.widgets.windows
  (:require [gdl.input :refer [key-just-pressed?]]
            [gdl.ui :as ui]
            [gdl.ui.actor :as actor]
            [moon.stage :as stage]
            [moon.component :as component]))

(defn create []
  (ui/group {:id :windows
             :actors [(component/create [:widgets/entity-info-window])
                      (component/create [:widgets/inventory])]}))

(defn- windows []
  (:windows (stage/get)))

(defn inventory [k]
  (get (windows) :inventory-window))

(defn check-hotkeys []
  (doseq [[hotkey window-id] {:keys/i :inventory-window
                              :keys/e :entity-info-window}
          :when (key-just-pressed? hotkey)]
    (actor/toggle-visible! (get (windows) window-id))))

(defn close-all []
  (let [windows (ui/children (windows))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible! % false) windows))))


