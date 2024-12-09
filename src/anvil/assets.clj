(ns anvil.assets
  (:require [clojure.gdx.asset-manager :as manager]
            [clojure.gdx.files :as files]
            [clojure.gdx.utils.disposable :as disposable]
            [clojure.string :as str]
            [clojure.utils :refer [defmethods]]))

(defn create [folder]
  (def manager (manager/load-all
                (for [[asset-type exts] [[:sound   #{"wav"}]
                                         [:texture #{"png" "bmp"}]]
                      file (map #(str/replace-first % folder "")
                                (files/recursively-search folder exts))]
                  [file asset-type]))))

(defn dispose []
  (disposable/dispose manager))
