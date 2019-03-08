port module App exposing (init, main)

import Browser
import Element exposing (..)
import Element.Background as Background
import Element.Border as Border
import Element.Events exposing (..)
import Element.Font as Font
import Element.Input as Input
import Html exposing (Html, div, img)
import Html.Attributes as HtmlAttr exposing (class, id, src)


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        }


port requestGraphData : () -> Cmd msg


port graphData : (String -> msg) -> Sub msg



-- MODEL


type alias Model =
    String


init : flags -> ( Model, Cmd Msg )
init flags =
    ( "", Cmd.none )



-- UPDATE


type Msg
    = ButtonClicked
    | GraphData Model


update : Msg -> Model -> ( Model, Cmd Msg )
update msg model =
    case msg of
        ButtonClicked ->
            ( model, requestGraphData () )

        GraphData data ->
            ( data, Cmd.none )



-- SUBSCRIPTIONS


subscriptions : Model -> Sub Msg
subscriptions model =
    graphData GraphData



-- VIEW


header : String -> Element msg
header title =
    row
        [ width fill
        , paddingXY 20 5
        , Border.widthEach { bottom = 1, top = 0, left = 0, right = 0 }
        , Border.color <| rgb255 200 200 200
        ]
        [ el [ centerX ] <| text title ]


sidebar : List String -> Element msg
sidebar variables =
    let
        varEl var =
            el [ paddingXY 15 5, width fill ] <| text var
    in
    column
        [ height fill
        , width <| fillPortion 1
        , paddingXY 0 10
        , Border.widthEach { bottom = 0, top = 0, left = 1, right = 0 }
        , Border.color <| rgb255 200 200 200
        ]
    <|
        List.map varEl variables


exposedDiv : String -> List (Attribute msg) -> List (Html msg) -> Element msg
exposedDiv id attrs children =
    el attrs <| html <| div [ HtmlAttr.id id ] children


graphPanel : Element Msg
graphPanel =
    let
        palette =
            exposedDiv "palette"
                []
                [ img
                    [ HtmlAttr.src "images/person.png"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                , img
                    [ HtmlAttr.src "images/sick.jpg"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                , img
                    [ HtmlAttr.src "images/happy.png"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                , img
                    [ HtmlAttr.src "images/virus.png"
                    , HtmlAttr.class "palette-img"
                    ]
                    []
                , img
                    [ HtmlAttr.id "redo_button"
                    , HtmlAttr.class "right"
                    , HtmlAttr.src "images/redo.png"
                    ]
                    []
                , img
                    [ HtmlAttr.id "undo_button"
                    , HtmlAttr.class "right"
                    , HtmlAttr.src "images/undo.png"
                    ]
                    []
                ]
    in
    el [ height fill, width <| fillPortion 4 ] <|
        column [ height fill, width fill ]
            [ palette
            , exposedDiv "graph" [ height <| px 600 ] []
            ]


view : Model -> Html Msg
view model =
    layout [ height fill ] <|
        column [ height fill, width fill ]
            [ header "Model Name"
            , row [ height fill, width fill ]
                [ graphPanel, sidebar [ "var 1", "var 2", "var 3" ] ]
            ]
