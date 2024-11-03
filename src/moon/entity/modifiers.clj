(ns moon.entity.modifiers
  (:require [moon.modifiers :as mods]))

(defn info [[_ mods]]
  (mods/info-text mods))

(defn handle [[k eid add-or-remove mods]]
  [[:e/assoc eid k ((case add-or-remove
                      :add    mods/add
                      :remove mods/remove) (k @eid) mods)]])
