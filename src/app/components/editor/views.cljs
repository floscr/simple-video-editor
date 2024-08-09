(ns app.components.editor.views
  (:require
   [uix.core :as uix :refer [$ defui]]))

(defui Editor []
  (let [[video-url set-video-url!] (uix/use-state nil)]
    ($ :div
       ($ :input
           {:type "file"
            :accept "video/*"
            :on-change (fn [e]
                         (let [file (-> e .-target .-files (aget 0))
                               url (js/URL.createObjectURL file)]
                           (set-video-url! url)))})
       (when video-url
          ($ :video
             {:src video-url
              :controls true
              :style {:max-width "100%"
                      :margin-top "10px"}})))))
