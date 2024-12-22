(ns anvil.world.tick.window-hotkeys
  (:require [anvil.controls :as controls]
            [anvil.world.tick :as tick]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group]
            [gdl.utils :refer [defn-impl]]))

(defn- check-window-hotkeys [stage]
  (doseq [window-id [:inventory-window
                     :entity-info-window]
          :when (key-just-pressed? (get controls/window-hotkeys window-id))]
    (actor/toggle-visible! (get (:windows stage) window-id))))

(defn- close-all-windows [stage]
  (let [windows (group/children (:windows stage))]
    (when (some actor/visible? windows)
      (run! #(actor/set-visible % false) windows))))

(defn-impl tick/window-hotkeys [stage]
  (check-window-hotkeys stage)
  (when (key-just-pressed? controls/close-windows-key)
    (close-all-windows stage)))
