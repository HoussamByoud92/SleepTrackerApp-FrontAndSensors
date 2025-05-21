-- phpMyAdmin SQL Dump
-- version 5.2.1
-- https://www.phpmyadmin.net/
--
-- Host: 127.0.0.1
-- Generation Time: May 21, 2025 at 03:17 PM
-- Server version: 10.4.28-MariaDB
-- PHP Version: 8.2.4

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Database: `sleep`
--

-- --------------------------------------------------------

--
-- Table structure for table `sleep_sessions`
--

CREATE TABLE `sleep_sessions` (
  `id` int(11) NOT NULL,
  `user_id` int(11) NOT NULL,
  `start` datetime NOT NULL,
  `stop` datetime NOT NULL,
  `events` text DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `sleep_sessions`
--

INSERT INTO `sleep_sessions` (`id`, `user_id`, `start`, `stop`, `events`) VALUES
(125, 3, '2025-05-17 22:00:00', '2025-05-18 06:00:00', '[\"Turned over at 02:14:31\",\"Sound detected at 01:42:21\"]'),
(126, 3, '2025-05-16 22:00:00', '2025-05-17 07:00:00', '[\"REM phase started at 01:41:44\",\"Snoring detected at 04:28:50\"]'),
(127, 3, '2025-05-15 22:00:00', '2025-05-16 05:00:00', '[\"Got out of bed at 04:04:38\",\"Noise outside at 02:22:13\"]'),
(128, 3, '2025-05-14 22:00:00', '2025-05-15 06:00:00', '[\"Movement detected at 00:44:01\"]'),
(129, 3, '2025-05-13 22:00:00', '2025-05-14 06:00:00', '[\"Heart rate spike at 03:53:07\",\"Sound monitoring started at 02:34:09\"]'),
(130, 3, '2025-05-12 22:00:00', '2025-05-13 06:00:00', '[\"REM phase started at 01:48:20\",\"Deep sleep at 04:45:33\"]'),
(131, 3, '2025-05-11 22:00:00', '2025-05-12 05:00:00', '[\"Light sleep at 02:11:15\"]'),
(132, 3, '2025-05-10 22:00:00', '2025-05-11 06:00:00', '[\"Turned over at 01:01:44\",\"Snoring detected at 02:30:29\"]'),
(133, 3, '2025-05-09 22:00:00', '2025-05-10 05:00:00', '[\"Breathing irregularity at 01:11:27\"]'),
(134, 3, '2025-05-08 22:00:00', '2025-05-09 05:00:00', '[\"Sound detected at 03:55:44\",\"Got out of bed at 01:48:31\"]'),
(135, 3, '2025-05-07 22:00:00', '2025-05-08 06:00:00', '[\"Noise outside at 00:39:12\"]'),
(136, 3, '2025-05-06 22:00:00', '2025-05-07 07:00:00', '[\"REM phase started at 03:17:53\",\"Movement detected at 01:37:11\"]'),
(137, 3, '2025-05-05 22:00:00', '2025-05-06 06:00:00', '[\"Snoring detected at 04:01:13\",\"Heart rate spike at 01:50:30\"]'),
(138, 3, '2025-05-04 22:00:00', '2025-05-05 06:00:00', '[\"Light sleep at 00:21:41\"]'),
(139, 3, '2025-05-03 22:00:00', '2025-05-04 06:00:00', '[\"Sound monitoring started at 03:42:11\",\"Got out of bed at 02:45:56\"]'),
(140, 3, '2025-05-02 22:00:00', '2025-05-03 07:00:00', '[\"REM phase started at 00:59:38\",\"Turned over at 03:28:47\"]'),
(141, 3, '2025-05-01 22:00:00', '2025-05-02 06:00:00', '[\"Deep sleep at 02:33:00\"]'),
(142, 3, '2025-04-30 22:00:00', '2025-05-01 05:00:00', '[\"Breathing irregularity at 01:44:12\",\"Movement detected at 03:21:14\"]'),
(143, 3, '2025-04-29 22:00:00', '2025-04-30 06:00:00', '[\"Snoring detected at 01:37:50\"]'),
(144, 3, '2025-04-28 22:00:00', '2025-04-29 06:00:00', '[\"Heart rate spike at 03:17:28\",\"Sound detected at 01:28:44\"]'),
(145, 3, '2025-04-27 22:00:00', '2025-04-28 06:00:00', '[\"Turned over at 02:59:30\",\"REM phase started at 04:23:21\"]'),
(146, 3, '2025-04-26 22:00:00', '2025-04-27 06:00:00', '[\"Noise outside at 00:45:33\"]'),
(147, 3, '2025-04-25 22:00:00', '2025-04-26 06:00:00', '[\"Light sleep at 01:15:22\",\"REM phase started at 02:36:47\"]'),
(148, 3, '2025-04-24 22:00:00', '2025-04-25 05:00:00', '[\"Got out of bed at 01:59:03\",\"Sound monitoring started at 00:31:52\"]'),
(149, 3, '2025-04-23 22:00:00', '2025-04-24 06:00:00', '[\"Movement detected at 03:04:19\"]'),
(150, 3, '2025-04-22 22:00:00', '2025-04-23 07:00:00', '[\"Snoring detected at 00:36:25\",\"Breathing irregularity at 04:29:00\"]'),
(151, 3, '2025-04-21 22:00:00', '2025-04-22 05:00:00', '[\"REM phase started at 01:00:49\"]'),
(152, 3, '2025-04-20 22:00:00', '2025-04-21 06:00:00', '[\"Heart rate spike at 01:39:45\"]'),
(153, 3, '2025-04-19 22:00:00', '2025-04-20 06:00:00', '[\"Turned over at 02:23:58\"]'),
(154, 3, '2025-04-18 22:00:00', '2025-04-19 05:00:00', '[\"Light sleep at 01:44:12\"]'),
(155, 3, '2025-04-17 22:00:00', '2025-04-18 07:00:00', '[\"Snoring detected at 00:14:39\",\"Sound detected at 03:33:26\"]'),
(156, 3, '2025-04-16 22:00:00', '2025-04-17 07:00:00', '[\"Turned over at 01:00:29\",\"Noise outside at 04:23:17\"]'),
(157, 3, '2025-04-15 22:00:00', '2025-04-16 07:00:00', '[\"REM phase started at 03:17:25\"]'),
(158, 3, '2025-04-14 22:00:00', '2025-04-15 05:00:00', '[\"Breathing irregularity at 01:19:34\",\"Light sleep at 01:35:33\"]'),
(159, 3, '2025-04-13 22:00:00', '2025-04-14 06:00:00', '[\"Heart rate spike at 00:53:00\"]'),
(160, 3, '2025-04-12 22:00:00', '2025-04-13 06:00:00', '[\"Turned over at 02:17:41\",\"Sound monitoring started at 03:47:14\"]'),
(161, 3, '2025-04-11 22:00:00', '2025-04-12 05:00:00', '[\"Noise outside at 04:11:12\"]'),
(162, 3, '2025-04-10 22:00:00', '2025-04-11 06:00:00', '[\"Snoring detected at 01:07:27\",\"Got out of bed at 04:31:34\",\"REM phase started at 00:26:18\"]'),
(163, 3, '2025-04-09 22:00:00', '2025-04-10 06:00:00', '[\"Deep sleep at 01:57:38\",\"Breathing irregularity at 03:38:19\"]'),
(164, 3, '2025-04-08 22:00:00', '2025-04-09 05:00:00', '[\"Sound detected at 03:10:25\"]'),
(165, 3, '2025-04-07 22:00:00', '2025-04-08 05:00:00', '[\"REM phase started at 01:45:43\",\"Light sleep at 23:20:20\"]'),
(166, 3, '2025-04-06 22:00:00', '2025-04-07 07:00:00', '[\"Turned over at 00:24:00\"]'),
(167, 3, '2025-04-05 22:00:00', '2025-04-06 07:00:00', '[\"Movement detected at 04:30:29\",\"Sound detected at 01:11:27\"]'),
(168, 3, '2025-04-04 22:00:00', '2025-04-05 06:00:00', '[\"Got out of bed at 03:34:37\",\"Heart rate spike at 01:47:00\",\"Noise outside at 00:23:58\"]'),
(169, 3, '2025-04-03 22:00:00', '2025-04-04 06:00:00', '[\"Sound detected at 02:00:48\"]'),
(170, 3, '2025-04-02 22:00:00', '2025-04-03 06:00:00', '[\"REM phase started at 00:39:33\",\"Deep sleep at 03:15:32\"]'),
(171, 3, '2025-04-01 22:00:00', '2025-04-02 06:00:00', '[\"Light sleep at 01:11:01\"]'),
(172, 3, '2025-03-31 22:00:00', '2025-04-01 06:00:00', '[\"Turned over at 04:12:59\",\"Breathing irregularity at 00:32:18\"]'),
(173, 3, '2025-03-30 22:00:00', '2025-03-31 05:00:00', '[\"Movement detected at 01:00:47\",\"Snoring detected at 02:35:44\"]'),
(174, 3, '2025-03-29 22:00:00', '2025-03-30 06:00:00', '[\"Sound detected at 01:24:33\"]'),
(175, 3, '2025-03-28 22:00:00', '2025-03-29 06:00:00', '[\"Heart rate spike at 04:38:00\",\"REM phase started at 01:59:41\"]'),
(176, 3, '2025-03-27 22:00:00', '2025-03-28 07:00:00', '[\"Snoring detected at 03:01:13\",\"Sound monitoring started at 01:47:17\"]'),
(177, 3, '2025-03-26 22:00:00', '2025-03-27 06:00:00', '[\"Deep sleep at 04:09:19\"]'),
(178, 3, '2025-03-25 22:00:00', '2025-03-26 05:00:00', '[\"Got out of bed at 01:14:56\",\"Light sleep at 23:12:37\"]'),
(179, 3, '2025-03-24 22:00:00', '2025-03-25 06:00:00', '[\"Turned over at 03:59:57\",\"REM phase started at 03:20:39\"]'),
(180, 3, '2025-03-23 22:00:00', '2025-03-24 06:00:00', '[\"Noise outside at 02:01:55\"]'),
(181, 3, '2025-03-22 22:00:00', '2025-03-23 06:00:00', '[\"Breathing irregularity at 00:13:44\",\"Heart rate spike at 02:14:56\"]'),
(182, 3, '2025-03-21 22:00:00', '2025-03-22 05:00:00', '[\"REM phase started at 00:36:43\"]'),
(183, 3, '2025-03-20 22:00:00', '2025-03-21 06:00:00', '[\"Turned over at 01:03:12\"]'),
(184, 3, '2025-03-19 22:00:00', '2025-03-20 06:00:00', '[]');

-- --------------------------------------------------------

--
-- Table structure for table `users`
--

CREATE TABLE `users` (
  `id` int(11) NOT NULL,
  `username` varchar(100) DEFAULT NULL,
  `email` varchar(100) DEFAULT NULL,
  `password` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;

--
-- Dumping data for table `users`
--

INSERT INTO `users` (`id`, `username`, `email`, `password`) VALUES
(3, 'hoss', 'hoss@hoss.hoss', '$2y$10$89VpEGXY3v2te3ykdP48fumsj3ZafnNZ2sbom4cSWRYfareB9pPBS');

--
-- Indexes for dumped tables
--

--
-- Indexes for table `sleep_sessions`
--
ALTER TABLE `sleep_sessions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `FK_HH` (`user_id`);

--
-- Indexes for table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`);

--
-- AUTO_INCREMENT for dumped tables
--

--
-- AUTO_INCREMENT for table `sleep_sessions`
--
ALTER TABLE `sleep_sessions`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=185;

--
-- AUTO_INCREMENT for table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- Constraints for dumped tables
--

--
-- Constraints for table `sleep_sessions`
--
ALTER TABLE `sleep_sessions`
  ADD CONSTRAINT `FK_HH` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
