(ns cdq.graphics.font
  (:require [clojure.graphics.freetype :as freetype]))

(defn create [graphics default-font]
  (assoc graphics :graphics/default-font (freetype/generate-font (:file-handle default-font)
                                                                 (:params default-font))))
