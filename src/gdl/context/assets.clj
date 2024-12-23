(ns gdl.context.assets
  (:require [gdl.assets :as assets]
            [gdl.context :as ctx]))

(defn setup [folder]
  (bind-root ctx/assets (assets/manager folder)))

(defn cleanup []
  (assets/cleanup ctx/assets))
