(ns app.assets
  (:require [clojure.string :as str]
            [utils.files :as files])
  (:import (com.badlogic.gdx.audio Sound)
           (com.badlogic.gdx.graphics Texture)))

(defn search [folder]
  (for [[class exts] [[Sound #{"wav"}]
                      [Texture #{"png" "bmp"}]]
        file (map #(str/replace-first % folder "")
                  (files/recursively-search folder exts))]
    [file class]))
