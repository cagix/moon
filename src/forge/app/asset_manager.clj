(ns forge.app.asset-manager
  (:require [clojure.string :as str]
            [clojure.gdx.asset-manager :as manager]
            [clojure.gdx.files :as files]
            [forge.core :refer [asset-manager bind-root dispose]]))

(defn create [[_ folder]]
  (bind-root asset-manager (manager/create))
  (manager/load asset-manager
                (for [[asset-type exts] [[:sound   #{"wav"}]
                                         [:texture #{"png" "bmp"}]]
                      file (map #(str/replace-first % folder "")
                                (files/recursively-search folder exts))]
                  [file asset-type])))

(defn destroy [_]
  (dispose asset-manager))
